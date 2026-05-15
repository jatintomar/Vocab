const https = require('https');

const options = {
  hostname: 'api.github.com',
  path: '/users/jatintomar028/gists',
  headers: {
    'User-Agent': 'Node-Fetch'
  }
};

https.get(options, (res) => {
  let data = '';
  res.on('data', (chunk) => {
    data += chunk;
  });
  res.on('end', () => {
    try {
      const gists = JSON.parse(data);
      console.log('Gists found:', gists.length);
      gists.forEach(gist => {
        console.log(`Gist: ${gist.id} - ${gist.description}`);
        Object.keys(gist.files).forEach(filename => {
          console.log(`  File: ${filename} -> ${gist.files[filename].raw_url}`);
        });
      });
    } catch (e) {
      console.error('Error parsing gists');
    }
  });
}).on('error', (e) => {
  console.error(e);
});
