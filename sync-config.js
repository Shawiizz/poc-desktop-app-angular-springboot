/**
 * Sync app.config.json to tauri.conf.json and copy favicon
 * Run before building: node sync-config.js
 */

const fs = require('fs');
const path = require('path');

const appConfigPath = path.join(__dirname, 'app.config.json');
const tauriConfigPath = path.join(__dirname, 'src-tauri', 'tauri.conf.json');
const faviconSource = path.join(__dirname, 'favicon.ico');
const faviconDestAngular = path.join(__dirname, 'frontend', 'public', 'favicon.ico');
const faviconDestTauri = path.join(__dirname, 'src-tauri', 'icons', 'icon.ico');

// Read configs
const appConfig = JSON.parse(fs.readFileSync(appConfigPath, 'utf-8'));
const tauriConfig = JSON.parse(fs.readFileSync(tauriConfigPath, 'utf-8'));

// Sync values
tauriConfig.productName = appConfig.name;
tauriConfig.version = appConfig.version;

// Ensure identifier format (com.example.app-id)
if (!tauriConfig.identifier.includes(appConfig.id)) {
  const prefix = tauriConfig.identifier.split('.').slice(0, -1).join('.');
  tauriConfig.identifier = `${prefix}.${appConfig.id}`;
}

// Sync window title
if (tauriConfig.app?.windows?.[0]) {
  tauriConfig.app.windows[0].title = appConfig.name;
}

// Write back
fs.writeFileSync(tauriConfigPath, JSON.stringify(tauriConfig, null, 2) + '\n');

// Copy favicon only if destination doesn't exist (allow customization)
if (fs.existsSync(faviconSource)) {
  let copied = [];
  
  if (!fs.existsSync(faviconDestAngular)) {
    fs.copyFileSync(faviconSource, faviconDestAngular);
    copied.push('frontend/public/');
  }
  
  if (!fs.existsSync(faviconDestTauri)) {
    fs.copyFileSync(faviconSource, faviconDestTauri);
    copied.push('src-tauri/icons/');
  }
  
  if (copied.length > 0) {
    console.log(`✓ Copied favicon.ico → ${copied.join(' & ')}`);
  } else {
    console.log('○ Favicon already exists in destinations (not overwritten)');
  }
}

console.log('✓ Synced app.config.json → tauri.conf.json');
console.log(`  Name: ${appConfig.name}`);
console.log(`  Version: ${appConfig.version}`);
console.log(`  Identifier: ${tauriConfig.identifier}`);
