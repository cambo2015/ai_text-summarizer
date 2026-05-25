import { Routes } from '@angular/router';

import { UserDashboard } from './components/user-dashboard/user-dashboard';
import { AdminDashboard } from './components/admin-dashboard/admin-dashboard';
import { Login } from './components/login/login';
import { Home } from './home/home';
import { roleGuard } from './core/guards/role-guard';
import { VerifyExpired } from './components/verify-expired/verify-expired';
import { VerifySuccess } from './components/verify-success/verify-success';
import { Settings } from './components/settings/settings';
import { Signup } from './components/signup/signup';

export const routes: Routes = [
  // ✅ public routes
  { path: "home", component: Home },
  { path: "login", component: Login },
  {path: "signup", component: Signup},
  { path: "verify-expired", component: VerifyExpired },
  { path: "verify-success", component: VerifySuccess },

  // 🔒 protected routes
  {
    path: "dashboard",
    component: UserDashboard,
    canActivate: [roleGuard],
    data: { roles: ["ROLE_USER", "ROLE_ADMIN"] }
  },
  { path: "settings", 
    component: Settings,
    canActivate: [roleGuard],
    data:{roles:['ROLE_USER']}
  },
  {
    path: "admin",
    component: AdminDashboard,
    canActivate: [roleGuard],
    data: { roles: ["ROLE_ADMIN"] }
  },

  // 👇 root redirects to home (PUBLIC)
  { path: "", redirectTo: "home", pathMatch: "full" },

  // 👇 wildcard (send unknown URLs somewhere sensible)
  { path: "**", redirectTo: "home" }
];

