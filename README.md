# Helpfix Fuel — Painel Automotivo 3.3.3

Projeto Android nativo em **Java**, sem dependências de interface externas. Criado para um painel **paisagem**, inspirado na referência visual fornecida pelo usuário: **dois grandes mostradores de ponteiro, iluminação ciano sobre fundo preto e painel central**.

## O que aparece na tela

- À esquerda: velocímetro analógico com ponteiro, escala de 0 a 240 km/h e velocidade GPS.
- À direita: relógio analógico com ponteiro para o percentual **estimado** de gasolina, de 0% a 100%.
- No meio: **autonomia estimada em km**, a partir dos litros indicados e do consumo médio.
- Embaixo: situação da viagem, distância GPS registrada, odômetro e litros estimados.
- A aba **MENU** fica recolhida na tela principal. Ao tocar, abre os controles de viagem, os ajustes de combustível e iluminação e a calculadora de custos.
- GPS usa o Android `LocationManager`, serviço em primeiro plano com notificação e painel opcional flutuante sobre aplicativos de navegação, mediante permissão do sistema.

### Importante sobre o combustível

O celular **não** lê o tanque nem o sensor do veículo. O usuário informa litros estimados, capacidade e consumo médio. A cada deslocamento GPS considerado válido, o aplicativo diminui os litros estimados proporcionalmente à distância e calcula a autonomia. Por isso, **não utilize os valores como medidor de combustível real**. Erros de GPS, consumo variável ou parâmetros incorretos afetam o resultado.

Configuração inicial ilustrativa: 15 L, tanque 46 L, 14 km/L. Ajuste no menu para seu carro. Não configure o aplicativo enquanto dirige.

## Odômetro e novo abastecimento

Use **MENU → Ajustar odômetro (km)** para digitar a quilometragem exibida no carro, por exemplo `202621`. O app soma a distância que o GPS registrar depois desse ajuste. O odômetro estimado é exibido na faixa inferior, ao lado da distância GPS. Você pode corrigi-lo a qualquer momento.

Em **MENU → Novo abastecimento**, informe o odômetro, os litros e o total pago ou o preço por litro. Confirme o odômetro com o valor do painel do carro. Ao salvar, os litros entram no saldo estimado do tanque e atualizam a autonomia. Marque **Completei o tanque** para ajustar o saldo à capacidade configurada. O registro também atualiza o histórico e o simulador de custo.

O painel principal preenche a área útil em paisagem, com as barras do Android recolhidas e o botão **MENU** no canto superior direito.

## Quanto custa rodar com o Celta?

Abra **MENU → 5. Quanto custa rodar com o Celta?**. Informe o preço da gasolina por litro e a distância total, incluindo a volta se houver. O simulador mostra litros, custo por quilômetro e custo de gasolina no percurso. Se você informar o valor da entrega, ele calcula quanto sobra depois da gasolina.

Em **Registrar**, informe o odômetro do carro, os litros e o valor pago. Marque **Completei o tanque** quando encher o tanque. Depois de dois registros de tanque cheio, a medição usa a distância entre eles e soma os litros e valores dos abastecimentos feitos depois do primeiro registro até o segundo, incluindo o segundo. A partir daí, a simulação usa o consumo medido. Até a primeira medição, ela usa o consumo médio configurado. O último abastecimento fornece o preço inicial da simulação, que pode ser editado.

O saldo estimado do tanque e o consumo usado pelo painel são editáveis em **Ajustar combustível e consumo**.

## Waze, Google Maps e avisos de radar

Abra **MENU → Navegar com Waze ou Google Maps**, informe o destino e toque no navegador que prefere usar. O Helpfix inicia o registro GPS se você tiver concedido a permissão de localização. Para ver o painel sobre a navegação, autorize a sobreposição no Android e marque a opção de painel flutuante. A escolha do endereço encontrado e da rota acontece no navegador.

Em **MENU → Avisos de radar**, baixe a base online, selecione 250, 400 ou 600 metros e salve com os avisos ativados. O Helpfix usa o GPS durante a viagem para mostrar radares fixos próximos à sua direção de deslocamento. A faixa aparece no painel principal e no flutuante. É possível ativar a leitura em voz alta em português. O limite só aparece quando consta na base. A base baixada funciona offline e pode ser atualizada manualmente.

A base inicial cobre os pontos mapeados em Porto Alegre e parte da Região Metropolitana. O Helpfix estima a aproximação em linha reta, por isso vias próximas ou paralelas podem gerar avisos. Confira a sinalização da via. Os avisos exibidos pelos navegadores seguem a configuração de cada um.

Dados de radares fixos © [colaboradores do OpenStreetMap](https://www.openstreetmap.org/copyright), licença ODbL, obtidos pela [Overpass API](https://wiki.openstreetmap.org/wiki/Overpass_API).

## Enviar ao GitHub pelo GitHub Desktop

1. Extraia este ZIP e copie **o conteúdo da pasta** para a raiz da cópia local do seu repositório, sem criar uma pasta interna extra.
2. No GitHub Desktop, confira os arquivos alterados, escreva a mensagem de commit e clique em **Commit to main**.
3. Clique em **Push origin**.
4. No navegador, abra **Actions > Helpfix Fuel - Compilar APK**, aguarde a execução verde, e baixe o artefato **HelpfixFuel-PainelAutomotivo-3.3.3-APK**.
5. Extraia o artefato baixado para obter `app-debug.apk`. A compilação ainda depende da execução real do GitHub Actions.

O workflow usa `android-actions/setup-android@v4` e Gradle 8.10.2. Ele instala o Android SDK 35 e executa `gradle --no-daemon --stacktrace assembleDebug`.

## Instalação e permissões

- A tela principal abre em **horizontal**. O menu permanece recolhido ao abrir.
- Toque em **MENU** para inserir combustível/capacidade/consumo e **INICIAR VIAGEM**.
- Permita acesso à localização para medir a velocidade pelo GPS.
- Para mostrar sobre Waze ou Maps, toque em **MOSTRAR SOBRE O WAZE / MAPS**, autorize "exibir sobre outros apps" e arraste o painel pela barra superior. O painel flutuante pode ser minimizado e fechado.
- Toque em **ENCERRAR VIAGEM** quando terminar.
- Um APK `debug` deve ser usado apenas para testes. Para distribuição são necessários testes reais e assinatura de release.

## Versões e instalação em paralelo

Este pacote usa `applicationId br.com.helpfix.fuelsport`, mesmo ID do Helpfix Fuel **Sport 3.2.3** para manter a linha de versão. Ele é separado do app original se este usar outro ID. A chave de assinatura debug do GitHub Actions pode variar entre compilações: se a instalação de uma atualização for recusada, desinstale **somente** a edição Sport, sabendo que seus dados locais serão perdidos, ou prepare uma assinatura persistente. Faça backup antes.

## Notas de implementação

- `app/src/main/java/br/com/helpfix/fuelsport/SportGaugeView.java`: instrumentos, ponteiros e desenho do painel em Canvas.
- `MainActivity.java`: tela imersiva, menu recolhido, GPS, configuração e acionamento do flutuante.
- `TrackingService.java`: GPS, percurso, serviço em primeiro plano e janela móvel.
- `FuelStore.java`: preferências locais.
- `.github/workflows/build-apk.yml`: compilação do APK.

**Status:** projeto-fonte e estrutura gerados; compilação e teste em aparelho precisam ser confirmados. Nenhum arquivo foi publicado no GitHub pela assistência.
