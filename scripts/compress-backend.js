/**
 * Compress backend binary using zstd
 * Cross-platform script for build pipelines
 * 
 * Usage: node compress-backend.js
 * 
 * Requires zstd to be installed:
 * - Windows: choco install zstd OR download from https://github.com/facebook/zstd/releases
 * - Linux: apt install zstd
 * - macOS: brew install zstd
 */

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const backendDir = path.join(__dirname, '..', 'launcher', 'backend');

// Platform-specific backend name
const isWindows = process.platform === 'win32';
const backendName = isWindows ? 'desktop-backend.exe' : 'desktop-backend';
const backendPath = path.join(backendDir, backendName);
const compressedPath = path.join(backendDir, `${backendName}.zst`);

// Check if backend exists
if (!fs.existsSync(backendPath)) {
  console.error(`✗ Backend not found: ${backendPath}`);
  console.error('  Run the native compilation first.');
  process.exit(1);
}

// Get original size
const originalSize = fs.statSync(backendPath).size;
console.log(`✓ Found backend: ${backendName}`);
console.log(`  Original size: ${(originalSize / 1024 / 1024).toFixed(2)} MB`);

// Compress with zstd --ultra -22 (maximum compression)
console.log('  Compressing with zstd --ultra -22 (maximum compression)...');

try {
  // Use zstd CLI for compression
  // --ultra = enable ultra mode (levels 20-22)
  // -22 = maximum compression level
  // -f = force overwrite
  // -o = output file
  const cmd = `zstd --ultra -22 -f "${backendPath}" -o "${compressedPath}"`;
  execSync(cmd, { stdio: 'inherit' });
  
  // Get compressed size
  const compressedSize = fs.statSync(compressedPath).size;
  const ratio = ((1 - compressedSize / originalSize) * 100).toFixed(1);
  
  console.log(`✓ Compressed successfully!`);
  console.log(`  Compressed size: ${(compressedSize / 1024 / 1024).toFixed(2)} MB`);
  console.log(`  Compression ratio: ${ratio}% reduction`);
  
  // Remove original to save space (we keep only the compressed version for embedding)
  fs.unlinkSync(backendPath);
  console.log(`✓ Removed original backend (keeping only compressed version)`);
  
} catch (error) {
  console.error('✗ Compression failed!');
  console.error('  Make sure zstd is installed:');
  console.error('  - Windows: choco install zstd');
  console.error('  - Linux: apt install zstd');
  console.error('  - macOS: brew install zstd');
  console.error('');
  console.error('  Error:', error.message);
  process.exit(1);
}
