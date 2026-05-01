import { existsSync, readFileSync } from 'node:fs';
import { join } from 'node:path';

const root = process.cwd().endsWith('sigcon-angular') ? process.cwd() : join(process.cwd(), 'sigcon-angular');

function readJson(relativePath) {
  const fullPath = join(root, relativePath);
  if (!existsSync(fullPath)) {
    throw new Error(`Missing required file: ${relativePath}`);
  }
  return JSON.parse(readFileSync(fullPath, 'utf8'));
}

function readText(relativePath) {
  const fullPath = join(root, relativePath);
  if (!existsSync(fullPath)) {
    throw new Error(`Missing required file: ${relativePath}`);
  }
  return readFileSync(fullPath, 'utf8');
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

const packageJson = readJson('package.json');
const angularJson = readJson('angular.json');
const proxyConfig = readJson('proxy.conf.json');
const styles = readText('src/styles.scss');
const tokens = readText('src/app/shared/design-tokens.scss');
const tailwind = readText('tailwind.config.js');

const dependencies = { ...packageJson.dependencies, ...packageJson.devDependencies };
[
  ['@angular/core', '^20.0.0'],
  ['@angular/cdk', '^20.0.3'],
  ['primeng', '^20.0.0'],
  ['@primeng/themes', '^20.0.0'],
  ['primeicons', '^7.0.0'],
  ['@azure/msal-angular', '^3.0.0'],
  ['@azure/msal-browser', '^3.0.0'],
  ['tailwindcss', '^3.4.0'],
  ['rxjs', '^7.8.0']
].forEach(([name, expected]) => {
  assert(dependencies[name] === expected, `${name} must be ${expected}`);
});

assert(angularJson.projects?.['sigcon-angular'], 'angular.json must define sigcon-angular project');
assert(angularJson.projects['sigcon-angular'].schematics?.['@schematics/angular:component']?.style === 'scss', 'components must default to SCSS');
assert(packageJson.scripts?.build === 'ng build', 'package.json must expose npm run build');
assert(packageJson.scripts?.test === 'ng test', 'package.json must expose npm test');

['/api', '/api-docs', '/swagger-ui.html', '/actuator'].forEach((path) => {
  assert(proxyConfig[path]?.target === 'http://localhost:8080', `${path} must proxy to backend local URL`);
});

[
  '--color-primary: #002869',
  '--color-primary-container: #0b3d91',
  '--color-secondary: #7e5700',
  '--color-secondary-container: #feb300',
  '--color-tertiary: #5f001b',
  '--color-surface: #f8f9ff',
  "--font-family: 'Public Sans', 'Inter', sans-serif"
].forEach((token) => {
  assert(tokens.includes(token), `Missing design token: ${token}`);
});

assert(styles.includes('@tailwind base'), 'styles.scss must include Tailwind base');
assert(styles.includes('@use "./app/shared/design-tokens.scss"'), 'styles.scss must load design tokens');
assert(tailwind.includes('./src/**/*.{html,ts,scss}'), 'tailwind.config.js must scan Angular sources');

console.log('SIGCON Angular bootstrap verification passed.');
