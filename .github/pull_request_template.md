## Qué hace este PR

<!-- Tres a cinco líneas: qué se agregó o cambió, y por qué. -->

## Checklist de terminado

<!-- Marca con N/A lo que no aplique a este PR. -->

- [ ] `.\mvnw.cmd clean install` en la raíz termina en `BUILD SUCCESS`
- [ ] El servicio arranca localmente con el perfil por defecto (H2), sin configurar nada
- [ ] La imagen se construye con `docker build` desde la raíz
- [ ] El contenedor arranca con el perfil `docker` contra MySQL y responde
- [ ] Ninguna contraseña real en ningún archivo del PR
- [ ] No hay archivos generados: ni `target/`, ni `.idea/`, ni `.vscode/`, ni `.env`
- [ ] La rama está actualizada con `develop`
