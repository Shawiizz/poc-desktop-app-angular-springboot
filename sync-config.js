/**
 * Sync app.config.json to tauri.conf.json and copy favicon
 * Run before building: node sync-config.js
 * Add --icons flag to regenerate all icon formats: node sync-config.js --icons
 */

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const appConfigPath = path.join(__dirname, 'app.config.json');
const tauriConfigPath = path.join(__dirname, 'launcher', 'tauri.conf.json');
const backendResourcesPath = path.join(__dirname, 'backend', 'main', 'resources');
const backendConfigDest = path.join(backendResourcesPath, 'app-config.json');
const faviconSource = path.join(__dirname, 'favicon.ico');
const faviconDestAngular = path.join(__dirname, 'frontend', 'public', 'favicon.ico');
const faviconDestTauri = path.join(__dirname, 'launcher', 'icons', 'icon.ico');

const generateIcons = process.argv.includes('--icons');

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

// Copy app.config.json to backend resources
if (!fs.existsSync(backendResourcesPath)) {
  fs.mkdirSync(backendResourcesPath, { recursive: true });
}
fs.copyFileSync(appConfigPath, backendConfigDest);
console.log('✓ Copied app.config.json → backend/main/resources/');

// Copy favicon only if destination doesn't exist (allow customization)
if (fs.existsSync(faviconSource)) {
  let copied = [];
  
  if (!fs.existsSync(faviconDestAngular)) {
    fs.copyFileSync(faviconSource, faviconDestAngular);
    copied.push('frontend/public/');
  }
  
  if (!fs.existsSync(faviconDestTauri)) {
    fs.copyFileSync(faviconSource, faviconDestTauri);
    copied.push('launcher/icons/');
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

// Generate all icon formats if --icons flag is passed
if (generateIcons && fs.existsSync(faviconSource)) {
  console.log('');
  console.log('Generating icon formats...');
  try {
    execSync(`cargo tauri icon "${faviconSource}"`, {
      cwd: path.join(__dirname, 'launcher'),
      stdio: 'inherit'
    });
    console.log('✓ Generated all icon formats');
  } catch (e) {
    console.error('✗ Failed to generate icons. Make sure cargo and tauri-cli are installed.');
    console.error('  Run: cargo install tauri-cli');
  }
}
