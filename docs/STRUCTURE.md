# Documentation Structure

## Overview

faktory-bot-ksp documentation is organized in `docs/` directory with clear separation of concerns.

## Directory Structure

```
docs/
├── README.md                           # Documentation index (start here)
├── STRUCTURE.md                        # This file - Documentation structure
├── guides/                             # User guides
│   ├── getting-started.md              # Quick start guide
│   ├── usage-guide.md                  # Detailed usage guide
│   └── troubleshooting.md              # Common issues and solutions
├── api/                                # API documentation
│   └── api-reference.md                # Complete API reference
├── modules/                            # Module documentation
│   ├── faktory-ksp.md                  # KSP processor module
│   └── faktory-runtime.md              # Runtime library module
├── design/                             # Design documents
│   └── dsl-design.md                   # DSL design decisions
└── development/                        # Development documentation
    ├── documentation-plan.md           # Documentation strategy
    └── documentation-completed.md      # Completion report
```

## File Organization

### docs/README.md
**Purpose:** Documentation index and navigation hub

**Content:**
- Quick navigation for new users, developers, and contributors
- Documentation structure overview
- Learning paths by user type
- Quick examples

**Target Audience:** All users (first entry point)

### guides/ - User Guides

#### getting-started.md
**Purpose:** Help new users get up and running quickly

**Content:**
- Installation steps
- Quick start tutorial
- First factory definition
- Basic usage examples
- Common setup issues

**Target Audience:** New users, beginners

#### usage-guide.md
**Purpose:** Detailed guide for daily usage

**Content:**
- DSL understanding and patterns
- Database persistence strategies
- Foreign key relationships
- Testing patterns
- Advanced usage

**Target Audience:** Regular users, intermediate level

#### troubleshooting.md
**Purpose:** Solve common problems quickly

**Content:**
- KSP code generation issues
- Compile errors
- jOOQ integration problems
- Runtime errors
- Testing issues
- Performance optimization

**Target Audience:** All users encountering problems

### api/ - API Documentation

#### api-reference.md
**Purpose:** Complete API specification

**Content:**
- All annotations (@Factory, @FactoryDsl)
- Generated DSL API
- Runtime classes (Factory, PersistableFactory)
- KSP processor internals
- Metadata extractors
- Code generators
- Type mappings
- Error handling

**Target Audience:** Developers needing detailed API information

### modules/ - Module Documentation

#### faktory-ksp.md
**Purpose:** KSP processor module internals

**Content:**
- KSP processor architecture
- Code generation mechanics
- Metadata extraction
- Foreign key detection
- Extension points
- Testing strategies

**Target Audience:** Contributors, advanced users

#### faktory-runtime.md
**Purpose:** Runtime library details

**Content:**
- @FactoryDsl annotation
- Factory base classes
- PersistableFactory
- FactoryBuilder interface
- Usage patterns
- Testing support

**Target Audience:** Users implementing custom factories

### design/ - Design Documents

#### dsl-design.md
**Purpose:** Document design decisions for DSL

**Content:**
- DSL architecture
- Constructor parameter pattern
- Alternative approaches comparison
- Type safety guarantees
- Trade-offs

**Target Audience:** Contributors, architects

### development/ - Development Documentation

#### documentation-plan.md
**Purpose:** Document the documentation strategy

**Content:**
- Documentation organization
- Update guidelines
- Quality standards

**Target Audience:** Documentation contributors

#### documentation-completed.md
**Purpose:** Document completion report

**Content:**
- Completed documentation list
- Coverage report
- Quality assurance results

**Target Audience:** Project maintainers

## Documentation Principles

### 1. Progressive Disclosure

- **Beginner:** Start with README.md → Getting Started → Usage Guide
- **Intermediate:** Usage Guide → API Reference → Module docs
- **Advanced:** Module docs → Design docs → Development docs

### 2. Single Source of Truth

- Each topic has one primary location
- Cross-references are used instead of duplication
- Updates are made in one place

### 3. Practical Examples

- Every concept includes working code examples
- Examples are tested and verified
- Real-world scenarios over abstract examples

### 4. Clear Navigation

- Every document links to related documents
- Breadcrumbs show document hierarchy
- Index provides quick access

## Learning Paths

### Path 1: New User

```
Root README.md
    ↓
docs/README.md (Documentation Index)
    ↓
docs/guides/getting-started.md
    ↓
docs/guides/usage-guide.md
    ↓
docs/guides/troubleshooting.md (as needed)
```

### Path 2: API Consumer

```
Root README.md
    ↓
docs/api/api-reference.md
    ↓
docs/guides/usage-guide.md (for patterns)
```

### Path 3: Contributor

```
Root README.md
    ↓
docs/modules/faktory-ksp.md
    ↓
docs/modules/faktory-runtime.md
    ↓
docs/design/dsl-design.md
    ↓
docs/development/documentation-plan.md
```

## Maintenance Guidelines

### When to Update

1. **New Feature:**
   - Add to API Reference
   - Update Usage Guide with examples
   - Add to Getting Started if affects setup
   - Update module docs if internals change

2. **Bug Fix:**
   - Update Troubleshooting if it solves a common issue
   - Update examples if they were incorrect

3. **Refactoring:**
   - Update module docs if architecture changes
   - Update Design docs if decisions change

### Quality Checklist

Before updating documentation:

- [ ] Code examples are tested and working
- [ ] Links are valid (no broken links)
- [ ] Language is clear and concise
- [ ] Follows existing document structure
- [ ] Cross-references are added where appropriate
- [ ] Target audience is clear

## Related Files

### Root Directory Documentation

- `/README.md` - Project overview and quick start
- `/CLAUDE.md` - Project instructions for AI agents
- `/USAGE.md` - Deprecated, replaced by docs/guides/usage-guide.md
- `/API_REFERENCE.md` - Deprecated, replaced by docs/api/api-reference.md

### Module Documentation

- `/faktory-ksp/README.md` - Deprecated, replaced by docs/modules/faktory-ksp.md
- `/faktory-runtime/README.md` - Deprecated, replaced by docs/modules/faktory-runtime.md

### Migration Status

Old documentation files are kept in root for backward compatibility but should reference the new docs/ structure.

## Future Enhancements

Planned documentation improvements:

1. **Interactive Examples** - Runnable code examples in browser
2. **Video Tutorials** - Step-by-step video guides
3. **FAQ Section** - Frequently asked questions
4. **Best Practices** - Curated patterns and anti-patterns
5. **Performance Guide** - Optimization strategies
6. **Migration Guide** - Upgrading between versions

## Contributing to Documentation

### Process

1. Identify documentation gap
2. Determine appropriate location (guides/api/modules/etc.)
3. Write content following existing style
4. Add cross-references
5. Test all code examples
6. Submit pull request

### Style Guide

- Use present tense ("user creates" not "user will create")
- Use active voice ("KSP generates" not "code is generated by KSP")
- Keep paragraphs short (3-5 lines)
- Use code blocks with language specification
- Include concrete examples over abstract explanations

## Contact

For documentation questions or suggestions:

- Open an issue: [GitHub Issues](https://github.com/krhrtky/faktory-bot-ksp/issues)
- Start a discussion: [GitHub Discussions](https://github.com/krhrtky/faktory-bot-ksp/discussions)
