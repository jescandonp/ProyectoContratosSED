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

Al 2026-05-06 el ultimo incremento cerrado es I5 en `feat/sigcon-i5`. El cierre tecnico de I5 esta en `bac3e5b` y la rama incluye catch-up documental posterior. Confirmar el estado exacto en `docs/plans/2026-05-04-sigcon-i5-execution-log.md` antes de continuar.

## Estructura

```text
ProyectoContratosSED/
├── docs/
│   ├── CONSTITUTION.md
│   ├── ARCHITECTURE.md
│   ├── TECNOLOGIAS.md
│   ├── ARRANQUE.md
│   ├── specs/
│   └── plans/
├── Prototipo/
├── Notas_ProyectoContratos/
└── Spec-Driven Development (SDD)_ A Comprehensive Technical Guide.pdf
```

Artefactos principales:

- `db/`
- `sigcon-backend/`
- `sigcon-angular/`
