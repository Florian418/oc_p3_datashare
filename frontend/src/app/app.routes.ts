import { Routes } from '@angular/router';
import { Upload } from './pages/upload/upload';
import { Login } from './pages/login/login';
import { Register } from './pages/register/register';

export const routes: Routes = [
  { path: '', component: Upload },
  { path: 'login', component: Login },
  { path: 'register', component: Register },
];
