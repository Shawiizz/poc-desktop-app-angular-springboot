/**
 * Compute MD5 hash of the backend binary
 * Cross-platform script for build pipelines
 * 
 * Usage: node compute-backend-hash.js
 */

const fs = require('fs');
const crypto = require('crypto');
const path = require('path');

const backendDir = path.join(__dirname, '..', 'launcher', 'backend');

// Platform-specific backend name
const isWindows = process.platform === 'win32';
const backendName = isWindows ? 'desktop-backend.exe' : 'desktop-backend';
const backendPath = path.join(backendDir, backendName);
const hashPath = path.join(backendDir, 'backend.hash');

if (!fs.existsSync(backendPath)) {
  console.error(`✗ Backend not found: ${backendPath}`);
  console.error('  Run the native compilation first.');
  process.exit(1);
}

// Compute MD5 hash
const fileBuffer = fs.readFileSync(backendPath);
const hash = crypto.createHash('md5').update(fileBuffer).digest('hex');

// Write hash file
fs.writeFileSync(hashPath, hash);

console.log(`✓ Computed hash for ${backendName}`);
console.log(`  Hash: ${hash}`);
console.log(`  Saved to: ${hashPath}`);
