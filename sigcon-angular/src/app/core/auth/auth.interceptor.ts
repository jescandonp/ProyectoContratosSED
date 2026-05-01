import { Injectable } from '@angular/core';
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { DevSessionService } from './dev-session.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private readonly devSession: DevSessionService) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const authorizationHeader = this.authorizationHeaderFor(request);
    const authorizedRequest = authorizationHeader
      ? request.clone({ setHeaders: { Authorization: authorizationHeader } })
      : request;
    return next.handle(authorizedRequest);
  }

  private authorizationHeaderFor(request: HttpRequest<unknown>) {
    if (!environment.useDevSession || !request.url.startsWith('/api')) {
      return null;
    }
    return this.devSession.authorizationHeader();
  }
}
