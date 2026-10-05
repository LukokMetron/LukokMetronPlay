# LukokMetron Android 2.0

Esta versão mantém o HTML do LukokMetron como interface, mas transfere a reprodução para um serviço Android em primeiro plano.

## O que mudou
- Widget controla **anterior / reproduzir-pausar / próxima** diretamente pelo serviço Android.
- Reprodução pode continuar com a tela bloqueada e com a Activity fechada.
- O Android usa `MediaPlayer` e `MediaSession` para a reprodução nativa.
- O botão de carregar pasta do HTML abre o seletor de pasta do Android.
- As músicas encontradas são guardadas no armazenamento privado de preferências do app como URIs persistentes.
- Estatísticas, favoritos, estrelas e demais regras continuam no HTML/localStorage.
- O APK continua podendo ser compilado pelo GitHub Actions, sem Android Studio.

## Limitação importante
A leitura da pasta nesta primeira versão procura arquivos de áudio diretamente dentro da pasta escolhida; subpastas não são percorridas. Se a sua coleção estiver organizada em subpastas, isso pode ser acrescentado.

## Compilar sem Android Studio
Envie o projeto para um repositório GitHub. Depois abra **Actions > Construir APK LukokMetron** e execute o workflow. O APK ficará em **Artifacts > LukokMetron-APK**.
