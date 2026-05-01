import { TestBed } from '@angular/core/testing';
import { HTTP_INTERCEPTORS, HttpClient, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';

import { AuthInterceptor } from './auth.interceptor';
import { DevSessionService } from './dev-session.service';

describe('AuthInterceptor', () => {
  let httpClient: HttpClient;
  let http: HttpTestingController;
  let devSession: DevSessionService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true }
      ]
    });
    httpClient = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
    devSession = TestBed.inject(DevSessionService);
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  it('adds Basic auth only to relative API requests in local-dev', () => {
    devSession.loginAs('ADMIN');

    httpClient.get('/api/usuarios/me').subscribe();
    const apiRequest = http.expectOne('/api/usuarios/me');
    expect(apiRequest.request.headers.get('Authorization')).toBe(`Basic ${btoa('admin@educacionbogota.edu.co:admin123')}`);
    apiRequest.flush({});

    httpClient.get('/assets/config.json').subscribe();
    const assetRequest = http.expectOne('/assets/config.json');
    expect(assetRequest.request.headers.has('Authorization')).toBeFalse();
    assetRequest.flush({});
  });
});
