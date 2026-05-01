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

El proyecto esta en fase previa a implementacion I1. La implementacion debe iniciar desde:

```text
docs/plans/2026-05-01-sigcon-i1-implementation-plan.md
```

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

Artefactos previstos por I1:

- `db/`
- `sigcon-backend/`
- `sigcon-angular/`
