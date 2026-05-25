import { routes } from './app.routes';

describe('app routes', () => {
  it('exposes the exact route surface implemented through I12', () => {
    const paths = collectUrlPaths(routes);

    expect(paths.length).toBe(23);
    expect(paths).toEqual(jasmine.arrayWithExactContents([
      '',
      'login',
      'perfil',
      'contratos',
      'contratos/:id',
      'contratos/:contratoId/informes/nuevo',
      'informes/:id',
      'informes/:id/preview',
      'informes/:id/corregir',
      'informes/:id/pdf',
      'notificaciones',
      'revision/informes',
      'aprobacion/informes',
      'admin',
      'admin/contratos',
      'admin/contratos/nuevo',
      'admin/contratos/:id/editar',
      'admin/usuarios',
      'admin/documentos-catalogo',
      'admin/busqueda',
      'visto-bueno',
      'visto-bueno/:id',
      '**'
    ]));
  });

  function collectUrlPaths(routeList: typeof routes, parent = ''): string[] {
    return routeList.flatMap((route) => {
      const current = parent && route.path ? `${parent}/${route.path}` : route.path ?? parent;
      if (route.children && route.path === '' && !route.redirectTo) {
        return collectUrlPaths(route.children, current);
      }
      if (route.children && route.path === '') {
        return [current, ...collectUrlPaths(route.children, current)];
      }
      return route.children ? collectUrlPaths(route.children, current) : [current];
    });
  }
});
