const fs = require('node:fs');
const path = require('node:path');
const sharp = require('C:/Users/XGamerXFuryX/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp');
const root = path.resolve(__dirname,'../icon');
async function preview() {
  const labels = '<svg width="1000" height="650"><g fill="#f5f5f5" font-family="Segoe UI,Arial" font-size="20"><text x="48" y="40">До</text><text x="388" y="40">После</text><text x="48" y="370">Размеры Android · 1:1</text><text x="48" y="620">36</text><text x="140" y="620">48</text><text x="240" y="620">72</text><text x="360" y="620">96</text><text x="520" y="620">144</text><text x="730" y="620">192 px</text></g></svg>';
  const layers = [{input:Buffer.from(labels),top:0,left:0}];
  for (const [file,left] of [['archive/v1/volume-control-1024.png',48],['volume-control-1024.png',388]]) {
    layers.push({input:await sharp(path.join(root,file)).resize(256,256).toBuffer(),top:64,left});
  }
  const sizes = [['ldpi',48],['mdpi',140],['hdpi',240],['xhdpi',360],['xxhdpi',520],['xxxhdpi',730]];
  for (const [density,left] of sizes) {
    layers.push({input:fs.readFileSync(path.join(root,`android/drawable-${density}/icon.png`)),top:400,left});
  }
  await sharp({create:{width:1000,height:650,channels:4,background:'#303030'}})
    .composite(layers).png().toFile(path.join(root,'preview.png'));
}
preview().catch(error => {console.error(error);process.exitCode=1;});
