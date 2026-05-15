const https = require('https');
const fs = require('fs');

const url = 'https://raw.githubusercontent.com/jatintomar028/JT-Vocab-Generator/main/data.json';

const download = (url, dest) => {
  return new Promise((resolve, reject) => {
    const file = fs.createWriteStream(dest);
    https.get(url, (response) => {
      if (response.statusCode === 302 || response.statusCode === 301) {
        download(response.headers.location, dest).then(resolve).catch(reject);
        return;
      }
      if (response.statusCode !== 200) {
        reject(new Error(`Failed to get '${url}' (status code: ${response.statusCode})`));
        return;
      }
      response.pipe(file);
      file.on('finish', () => {
        file.close();
        resolve();
      });
    }).on('error', (err) => {
      fs.unlink(dest, () => {});
      reject(err);
    });
  });
};

download(url, 'downloaded_data.json')
  .then(() => console.log('✅ Download Successful'))
  .catch((err) => console.error('❌ Error:', err.message));
