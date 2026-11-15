# Helpfix Fuel Sport — 3.2.3

Projeto **Android nativo em Java** para um **novo repositório**, independente do app Helpfix Fuel instalado anteriormente.

## O que contém
- Painel esportivo com **3 relógios analógicos**: velocidade (GPS), combustível estimado e autonomia estimada.
- Três estilos de cor: Racing vermelho, GT azul e Sport verde.
- Painel flutuante sobre Waze/Google Maps (após permissão do Android).
- Painel arrastável, minimizável e ocultável.
- Controle de início/fim da viagem e execução de localização em serviço em primeiro plano.
- Campos para informar litros atuais, consumo (km/L) e capacidade do tanque.
- Distância percorrida acumulada e desconto estimado do combustível pelo GPS.
- GitHub Actions configurado para gerar APK debug em cada push.

## Atenção aos limites
- **Projeto independente**: não preserva automaticamente os módulos, histórico e interface da versão 3.2.2 existente no repositório antigo. Use-o para um novo app/novo repositório, conforme solicitado.
- Combustível não é medido por sensor/OBD: é uma estimativa baseada na distância GPS e no consumo informado. Para medir corretamente, ajuste os litros após cada abastecimento.
- GPS depende da permissão e da qualidade do sinal; o sistema pode pausar a aplicação dependendo das restrições de bateria. Não use os valores como substituto de medidores oficiais do carro.
- O arquivo **não é APK pronto**: a compilação será feita pelo GitHub Actions. O build não foi executado no ambiente de criação deste ZIP.
- O APK de depuração é assinado automaticamente pelo executor e pode ter uma chave diferente em builds futuros; não conte com atualização por cima. Para atualizações consistentes, configure assinatura de release persistente.
- O app precisa de testes em aparelho real, especialmente overlays e permissões Android recentes.

## Como enviar pelo GitHub Desktop
1. No GitHub Desktop, **File → Clone repository** e escolha o repositório novo que você criou.
2. Descompacte este ZIP e copie **o conteúdo da pasta** para a raiz do repositório clonado (não a pasta inteira).
3. Na aba **Changes**, insira em Summary: `Helpfix Fuel Sport 3.2.3 - painel esportivo`.
4. Clique em **Commit to main** e depois em **Push origin**.
5. Na página do repositório GitHub, entre em **Actions → Build APK - Helpfix Fuel Sport**.
6. Clique no workflow finalizado e, em **Artifacts**, baixe `HelpfixFuelSport-3.2.3-debug-apk`.
7. Extraia o artifact e instale `app-debug.apk` no Android para testes. Instalação de fontes desconhecidas pode exigir autorização.

**Importante:** o repositório é seu. Este pacote não foi publicado nem enviado automaticamente ao GitHub.
