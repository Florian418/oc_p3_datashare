import http from 'k6/http';
import { check, sleep } from 'k6';

// Charge utile synthétique (5 Mo) : en-tête PNG valide (8 octets magic bytes, suffisant pour
// que Tika/FileTypeValidator l'accepte côté backend) suivi de zéros — même logique que le PNG
// minimal utilisé dans e2e/tests/upload.spec.ts, juste dimensionné pour représenter un vrai
// transfert réseau/disque plutôt qu'un fichier de quelques octets.
const FILE_SIZE_BYTES = 5 * 1024 * 1024;
const PNG_HEADER = [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a];
const SHARE_PASSWORD = 'perf-test-password';

function buildFilePayload() {
    const bytes = new Uint8Array(FILE_SIZE_BYTES);
    bytes.set(PNG_HEADER);
    return bytes.buffer;
}

const FILE_PAYLOAD = buildFilePayload();
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
    stages: [
        { duration: '10s', target: 10 }, // montée en charge
        { duration: '30s', target: 10 }, // palier
        { duration: '10s', target: 0 },  // redescente
    ],
    thresholds: {
        http_req_duration: ['p(95)<2000'],
        http_req_failed: ['rate<0.01'],
    },
};

// exécuté une seule fois avant le test : un compte de test partagé par tous les VUs, pour
// pouvoir uploader/télécharger/supprimer en authentifié sans multiplier les inscriptions
export function setup() {
    const email = `perf-${Date.now()}@datashare.test`;
    const password = 'perf-test-password';

    const registerRes = http.post(`${BASE_URL}/api/v1/auth/register`, JSON.stringify({ email, password }), {
        headers: { 'Content-Type': 'application/json' },
    });
    check(registerRes, { 'register status is 201': (r) => r.status === 201 });

    const loginRes = http.post(`${BASE_URL}/api/v1/auth/login`, JSON.stringify({ email, password }), {
        headers: { 'Content-Type': 'application/json' },
    });
    check(loginRes, { 'login status is 200': (r) => r.status === 200 });

    return { authToken: JSON.parse(loginRes.body).token };
}

export default function (data) {
    const authHeaders = { headers: { Authorization: `Bearer ${data.authToken}` } };

    // 1. upload authentifié, protégé par mot de passe
    const uploadRes = http.post(
        `${BASE_URL}/api/v1/files`,
        {
            file: http.file(FILE_PAYLOAD, 'perf-test.png', 'image/png'),
            expiresInDays: '7',
            password: SHARE_PASSWORD,
        },
        authHeaders,
    );
    const uploadOk = check(uploadRes, { 'upload status is 201': (r) => r.status === 201 });
    if (!uploadOk) {
        sleep(1);
        return;
    }
    const shareToken = JSON.parse(uploadRes.body).token;

    // 2. authentification sur le partage (mot de passe → token d'accès éphémère)
    const authenticateRes = http.post(
        `${BASE_URL}/api/v1/shares/${shareToken}/authenticate`,
        JSON.stringify({ password: SHARE_PASSWORD }),
        { headers: { 'Content-Type': 'application/json' } },
    );
    const authenticateOk = check(authenticateRes, { 'authenticate status is 200': (r) => r.status === 200 });

    // 3. téléchargement réel du contenu
    if (authenticateOk) {
        const accessToken = JSON.parse(authenticateRes.body).accessToken;
        const downloadRes = http.get(`${BASE_URL}/api/v1/shares/${shareToken}/download?access_token=${accessToken}`);
        check(downloadRes, {
            'download status is 200': (r) => r.status === 200,
            'download has full content': (r) => r.body.length === FILE_SIZE_BYTES,
        });
    }

    // 4. suppression (nécessite l'id interne, pas le token public — recherché via l'historique)
    const listRes = http.get(`${BASE_URL}/api/v1/files`, authHeaders);
    const listOk = check(listRes, { 'list status is 200': (r) => r.status === 200 });
    if (listOk) {
        const fileEntry = JSON.parse(listRes.body).find((item) => item.token === shareToken);
        if (fileEntry) {
            const deleteRes = http.del(`${BASE_URL}/api/v1/files/${fileEntry.id}`, null, authHeaders);
            check(deleteRes, { 'delete status is 204': (r) => r.status === 204 });
        }
    }

    sleep(1);
}
