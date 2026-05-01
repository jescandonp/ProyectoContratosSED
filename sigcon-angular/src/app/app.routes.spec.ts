import { routes } from './app.routes';

describe('app routes', () => {
  it('exposes the exact I1 route surface', () => {
    const paths = collectUrlPaths(routes);

    expect(paths.length).toBe(12);
    expect(paths).toEqual(jasmine.arrayWithExactContents([
      '',
      'login',
      'perfil',
      'contratos',
      'contratos/:id',
      'admin',
      'admin/contratos',
      'admin/contratos/nuevo',
      'admin/contratos/:id/editar',
      'admin/usuarios',
      'admin/documentos-catalogo',
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
