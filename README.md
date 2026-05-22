# ProyectoContratosSED

SIGCON es el Sistema de Gestion de Contratos para la Secretaria de Educacion del Distrito, estructurado bajo Spec-Driven Development (SDD) en nivel Spec-Anchored.

## Entrada Documental

| Documento | Proposito |
|-----------|-----------|
| `docs/CONSTITUTION.md` | Reglas SDD, autoridad de artefactos, gates y limites por incremento |
| `docs/ARCHITECTURE.md` | Arquitectura tecnica SIGCON/SED |
| `docs/TECNOLOGIAS.md` | Versiones canonicas del stack |
| `docs/ARRANQUE.md` | Guia de arranque local y estado operativo |
| `docs/specs/` | PRD y specs tecnicas por incremento |
| `docs/plans/` | Planes ejecutables y outlines por incremento |

## Estado

El proyecto se implementa por incrementos bajo SDD Spec-Anchored. El incremento activo, la rama vigente, el ultimo punto de retoma y las validaciones obligatorias se consultan siempre en el execution log del incremento correspondiente:

```text
docs/plans/
```

**Ultimo incremento cerrado: I11 — Correcciones Formato PDF 11-IF-023 V1** en rama `main` (2026-05-22).

| Incremento | Descripcion | Estado |
|-----------|-------------|--------|
| I1 | Autenticacion, usuarios, contratos, catalogo | Cerrado |
| I2 | Informes, actividades, soportes, revision | Cerrado |
| I3 | Aprobacion final, PDF, notificaciones | Cerrado |
| I4 | Revisor opcional, contrato editable, periodo informe | Cerrado |
| I5 | Edicion de actividades en BORRADOR | Cerrado |
| I6 | SGSSI, datos desembolso, PDF institucional | Cerrado |
| I7 | Usuario IVA, documentos requeridos, email, busqueda | Cerrado |
| I8 | fechaElaboracion, PDF formato 11-IF-023 V1 | Cerrado |
| I9 | Visto Bueno Administrativo | Cerrado |
| I10 | Identidad Visual Prensa SED | Cerrado |
| I11 | Correcciones Formato PDF 11-IF-023 V1 | Cerrado |

## Estructura

```text
ProyectoContratosSED/
├── docs/
│   ├── CONSTITUTION.md
│   ├── ARCHITECTURE.md
│   ├── TECNOLOGIAS.md
│   ├── ARRANQUE.md
│   ├── GUIA_PRUEBAS_FUNCIONALES.md
│   ├── specs/
│   └── plans/
├── db/
├── sigcon-backend/
├── sigcon-angular/
├── Prototipo/
└── Notas_ProyectoContratos/
```
