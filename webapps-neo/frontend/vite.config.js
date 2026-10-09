import { defineConfig } from 'vite';
import preact from '@preact/preset-vite';

// https://vitejs.dev/config/
export default defineConfig({
	plugins: [preact()],
	// The Maven assembly (webapps-neo/assembly) packages the build from
	// frontend/target, and the root pom's clean plugin cleans that directory.
	build: {
		outDir: 'target',
		emptyOutDir: true,
	},
	server: {
		host: '127.0.0.1',
		proxy: {
			// '/api': 'http://localhost:8084',
			'/api': {
				target: 'http://localhost:8084',
				changeOrigin: true,
				rewrite: (path) => path.replace(/^\/api/, ''),
			}
		}
	}


});
