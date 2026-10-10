import { readFileSync, readdirSync, existsSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const repository = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const statuses = { documented: 'Documentée', observed: 'Observée', proposed: 'Proposée' };

function filesUnder(directory) {
  return readdirSync(directory, { withFileTypes: true }).flatMap(entry => {
    const filename = path.join(directory, entry.name);
    return entry.isDirectory() ? filesUnder(filename) : [filename];
  });
}

export function validateManifest(root, manifest) {
  const errors = [];
  const ids = new Set();
  const referenced = new Set();
  const functionalRoot = path.join(root, 'docs/functional');
  const specifications = new Map(filesUnder(functionalRoot).filter(file => file.endsWith('.md'))
    .map(file => [path.relative(root, file).replaceAll('\\', '/'), readFileSync(file, 'utf8')]));
  const definitions = new Map();
  for (const [file, content] of specifications) {
    for (const match of content.matchAll(/^### ([A-Z]+-\d{3}) — .+\r?\n([\s\S]*?)(?=^### |^## |$(?![\s\S]))/gm)) {
      if (definitions.has(match[1])) errors.push(`ID défini plusieurs fois : ${match[1]}`);
      definitions.set(match[1], { file, content: match[2] });
    }
  }
  for (const [suite, filename] of Object.entries(manifest.suites ?? {})) {
    if (!filename.startsWith('app/src/test/') && !filename.startsWith('client/tests/')) {
      errors.push(`Suite hors des répertoires de tests : ${suite}`);
    }
    if (!existsSync(path.join(root, filename))) errors.push(`Suite introuvable : ${suite} (${filename})`);
  }
  if (!Array.isArray(manifest.rules) || manifest.rules.length === 0) errors.push('Matrice de règles vide');
  for (const rule of manifest.rules ?? []) {
    if (ids.has(rule.id)) errors.push(`ID dupliqué dans la matrice : ${rule.id}`);
    ids.add(rule.id);
    const definition = definitions.get(rule.id);
    if (!definition || definition.file !== rule.spec) errors.push(`Fiche ou règle introuvable : ${rule.id}`);
    if (!statuses[rule.status] || !definition?.content.includes(`Statut : ${statuses[rule.status]}.`)) {
      errors.push(`Statut incohérent : ${rule.id}`);
    }
    if (!['automated', 'partial', 'gap'].includes(rule.coverage)) errors.push(`Couverture invalide : ${rule.id}`);
    if (rule.coverage !== 'automated' && !rule.gap?.trim()) errors.push(`Lacune non expliquée : ${rule.id}`);
    if (rule.coverage !== 'gap' && !rule.tests?.length) errors.push(`Test manquant : ${rule.id}`);
    for (const test of rule.tests ?? []) {
      const filename = manifest.suites?.[test.suite];
      referenced.add(test.suite);
      if (!filename || !existsSync(path.join(root, filename))) {
        errors.push(`Référence de suite invalide : ${rule.id}/${test.suite}`);
        continue;
      }
      const content = readFileSync(path.join(root, filename), 'utf8');
      if (filename.endsWith('.java')) {
        if (!test.method || !/^[A-Za-z]\w*$/.test(test.method)
            || !new RegExp(`\\bvoid\\s+${test.method}\\s*\\(`).test(content)) {
          errors.push(`Méthode de test introuvable : ${rule.id}/${test.method}`);
        }
      } else if (!test.title || !content.includes(`test('${test.title}'`)) {
        errors.push(`Parcours navigateur introuvable : ${rule.id}/${test.title}`);
      }
    }
  }
  for (const id of definitions.keys()) if (!ids.has(id)) errors.push(`Règle sans matrice : ${id}`);
  for (const suite of Object.keys(manifest.suites ?? {})) {
    if (!referenced.has(suite)) errors.push(`Suite non référencée : ${suite}`);
  }
  return errors;
}

export function validateLinks(root) {
  const errors = [];
  const files = [path.join(root, 'README.md'), path.join(root, 'AGENTS.md'), ...filesUnder(path.join(root, 'docs'))]
    .filter(file => file.endsWith('.md'));
  for (const file of files) {
    for (const match of readFileSync(file, 'utf8').matchAll(/\[[^\]]+\]\(([^)]+)\)/g)) {
      const target = match[1].split('#')[0];
      if (!target || /^[a-z][a-z0-9+.-]*:/i.test(target)) continue;
      if (!existsSync(path.resolve(path.dirname(file), decodeURIComponent(target)))) {
        errors.push(`Lien local introuvable : ${path.relative(root, file)} → ${target}`);
      }
    }
  }
  return errors;
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    const manifest = JSON.parse(readFileSync(path.join(repository, 'docs/functional/coverage.json'), 'utf8'));
    const errors = [...validateManifest(repository, manifest), ...validateLinks(repository)];
    if (errors.length) {
      console.error(errors.join('\n'));
      process.exitCode = 1;
    } else {
      const gaps = manifest.rules.filter(rule => rule.coverage !== 'automated');
      console.log(`${manifest.rules.length} règles et leurs références vérifiées ; ${gaps.length} couvertures partielles ou absentes.`);
      for (const rule of gaps) console.log(`- ${rule.id} (${rule.coverage}) : ${rule.gap}`);
    }
  } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
  }
}
