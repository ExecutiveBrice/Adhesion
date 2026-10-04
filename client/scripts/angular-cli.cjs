// Avoid native Sass process startup failures in restricted Windows environments.
// Other platforms and explicit compiler settings keep Angular's default behavior.
if (process.platform === 'win32' && process.env.NG_BUILD_SASS_EMBEDDED === undefined) {
  process.env.NG_BUILD_SASS_EMBEDDED = 'false';
}

require('../node_modules/@angular/cli/bin/ng.js');
