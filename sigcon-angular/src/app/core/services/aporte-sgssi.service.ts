import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { AporteSgssiDto, AporteSgssiRequest } from '../models/aporte-sgssi.model';

@Injectable({ providedIn: 'root' })
export class AporteSgssiService {
  constructor(private readonly http: HttpClient) {}

  listar(informeId: number) {
    return this.http.get<AporteSgssiDto[]>(`/api/informes/${informeId}/aportes-sgssi`);
  }

  guardarTodos(informeId: number, aportes: AporteSgssiRequest[]) {
    return this.http.put<AporteSgssiDto[]>(`/api/informes/${informeId}/aportes-sgssi`, aportes);
  }
}
