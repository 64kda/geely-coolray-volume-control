// The SVG master is the only source for all launcher densities and large exports.
const fs = require('node:fs');
const path = require('node:path');
const sharp = require('C:/Users/XGamerXFuryX/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp');
const iconRoot = path.resolve(__dirname, '../icon');
const densities = [['ldpi',36],['mdpi',48],['hdpi',72],['xhdpi',96],['xxhdpi',144],['xxxhdpi',192]];

async function buildIcon() {
  const source = fs.readFileSync(path.join(iconRoot, 'volume-control.svg'));
  const outputs = [
    ['volume-control.png',512], ['volume-control-1024.png',1024], ['volume-control-2048.png',2048],
    ...densities.map(([density,size]) => [`android/drawable-${density}/icon.png`,size]),
  ];
  for (const [file,size] of outputs) {
    const target = path.join(iconRoot,file);
    fs.mkdirSync(path.dirname(target),{recursive:true});
    // Render vectors above the target size before downsampling; never upscale a PNG.
    await sharp(source,{density:Math.ceil(72*Math.max(512,size*2)/512)})
      .resize(size,size,{kernel:'lanczos3'}).png().toFile(target);
  }
  console.log('Icon: SVG master, 512/1024/2048 PNG, all six Android densities exported.');
}
module.exports = buildIcon;
if (require.main === module) buildIcon().catch(error => {console.error(error);process.exitCode=1;});
