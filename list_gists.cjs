const https = require('https');

const options = {
  hostname: 'api.github.com',
  path: '/users/jatintomar028/gists',
  headers: {
    'User-Agent': 'Node.js'
  }
};

https.get(options, (res) => {
  let data = '';
  res.on('data', (chunk) => { data += chunk; });
  res.on('end', () => {
    try {
      const gists = JSON.parse(data);
      if (Array.isArray(gists)) {
        gists.forEach(gist => {
          console.log(`Gist: ${gist.id} - ${gist.description || 'No description'}`);
          for (const file in gist.files) {
            console.log(`  File: ${file} -> ${gist.files[file].raw_url}`);
          }
        });
      } else {
        console.log('No gists found or error in response');
      }
    } catch (e) {
      console.error('Error parsing response');
    }
  });
}).on('error', (e) => {
  console.error(e);
});
