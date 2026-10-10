import { after, test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtempSync, mkdirSync, writeFileSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import path from 'node:path';
import { validateManifest } from './check-specs.mjs';

const root = mkdtempSync(path.join(tmpdir(), 'adhesion-specs-'));
mkdirSync(path.join(root, 'docs/functional'), { recursive: true });
mkdirSync(path.join(root, 'app/src/test'), { recursive: true });
writeFileSync(path.join(root, 'docs/functional/access.md'), '# Accès\n\n### ACCESS-001 — Protection\n\nStatut : Observée.\n');
writeFileSync(path.join(root, 'app/src/test/AccessTest.java'), 'class AccessTest { @Test void rejectsForeignSection() {} }');
after(() => rmSync(root, { recursive: true, force: true }));

function manifest() {
  return { suites: { access: 'app/src/test/AccessTest.java' }, rules: [{ id: 'ACCESS-001',
    spec: 'docs/functional/access.md', status: 'observed', coverage: 'automated',
    tests: [{ suite: 'access', method: 'rejectsForeignSection' }] }] };
}

test('accepts a rule linked to an existing named test', () => {
  assert.deepEqual(validateManifest(root, manifest()), []);
});
test('rejects a deleted or renamed test method', () => {
  const data = manifest(); data.rules[0].tests[0].method = 'removedMethod';
  assert.match(validateManifest(root, data).join('\n'), /Méthode de test introuvable/);
});
test('rejects silent promotion of observed behavior and duplicate IDs', () => {
  const data = manifest(); data.rules[0].status = 'documented'; data.rules.push(data.rules[0]);
  const errors = validateManifest(root, data).join('\n');
  assert.match(errors, /Statut incohérent/); assert.match(errors, /ID dupliqué/);
});
test('rejects an untracked rule or an unexplained coverage gap', () => {
  const data = manifest(); data.rules[0].coverage = 'gap'; data.rules[0].tests = [];
  assert.match(validateManifest(root, data).join('\n'), /Lacune non expliquée/);
  data.rules = [];
  assert.match(validateManifest(root, data).join('\n'), /Règle sans matrice/);
});
