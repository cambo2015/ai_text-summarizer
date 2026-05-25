import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../../services/auth-service';
import { filter, map, take } from 'rxjs';

export const roleGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const needed = (route.data?.['roles'] as string[]) ?? [];

  return auth.ensureRoles().pipe(
    // ⛔ DO NOT emit while loading
    filter(roles => roles !== null),

    map(roles => {
      // ❌ not logged in → redirect
      if (roles.length === 0) {
        return router.createUrlTree(
          ['/login'],
          { queryParams: { returnUrl: state.url } }
        );
      }

      // ✅ no role restriction
      if (needed.length === 0) {
        return true;
      }

      // 🔐 role check
      const allowed = roles.some(r => needed.includes(r));
      return allowed
        ? true
        : router.createUrlTree(['/login']);
    }),

    take(1)
  );
};
