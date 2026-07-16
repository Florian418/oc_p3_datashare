import { Routes } from '@angular/router';
import { Upload } from './pages/upload/upload';
import { Login } from './pages/login/login';
import { Register } from './pages/register/register';
import { Download } from './pages/download/download';
import { MySpace } from './pages/my-space/my-space';
import { FileDetail } from './pages/file-detail/file-detail';
import { NotFound } from './pages/not-found/not-found';
import { authGuard } from './auth/auth.guard';

export const routes: Routes = [
  { path: '', component: Upload },
  { path: 'login', component: Login },
  { path: 'register', component: Register },
  { path: 'download/:token', component: Download },
  { path: 'my-space', component: MySpace, canActivate: [authGuard] },
  { path: 'my-space/:id', component: FileDetail, canActivate: [authGuard] },
  { path: '**', component: NotFound },
];
