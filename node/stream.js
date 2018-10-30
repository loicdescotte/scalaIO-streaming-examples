const http = require('http');
const url  = require('url');

http.createServer(function (req, res) {
	const url_parts = url.parse(req.url, true);
	const query = url_parts.query;
	const keyword = query.keyword;
	const names = ['Bob', 'John', 'Martin']

    res.writeHead(200, {'Content-Type': 'text/plain'});

    const findName = () => names[Math.floor(Math.random() * names.length)]
    
    setInterval(() => {      
      res.write(`{"text":"${keyword} is great!","author":"${findName()}"}`);
      res.write('\n');    

      res.write(`{"text":"I love ${keyword}!","author":"${findName()}"}`);
      res.write('\n');

	  res.write(`{"text":"${keyword} is the best!","author":"${findName()}"}`);
      res.write('\n');
    }, 2000); 

}).listen(3000);