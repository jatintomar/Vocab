(async () => {
    try {
        const res = await fetch('https://api.github.com/users/jatintomar028/repos', {
            headers: { 'User-Agent': 'Node-Fetch' }
        });
        if (!res.ok) {
           console.log("Error:", res.status, res.statusText);
           return;
        }
        const data = await res.json();
        console.log("Repos:", data.map(repo => repo.name).join(', '));
    } catch (err) {
        console.log(err);
    }
})();
