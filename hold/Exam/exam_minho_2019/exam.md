## Construa uma execução concorrente para um conjunto que ilustre uma semantica do tipo add-wins set. Indentifique as operações ocorridas e qual o resultado observado no conjunto,

Assumindo que:

- add(x): Escreve o elemento X
- rmv(x): Remove o elemento X

### Operações sequenciais no servidor

Estado inicial: {}

Suponha que num servidor central são agora executados os seguintes comandos:
Ação: addX) -> add(Y)

Estado final: {X, Y}

### Execuções concorrentes, após execuções sequenciais

Suponha-se que existem agora duas execuções concorrentes A e B, que surgem
depois do estado ser {X, Y}

A: {X, Y} -> add(Y) -> rmv(X) -> {Y} B: {X, Y} -> add(X) -> rmv(Y) -> {X}

Após ser feito merge do A e B, vai ser obtido {X, Y}, uma vez que após a adição
desses valores, não existiu uma remove não concorrente.

O set final possui elementos e que foram adicionados através da operação add(e)
e nenhum desses add(e) aconteceu antes de um rmv(e) (add(e) ≺ rmv(e))

Outro exemplo, partindo de {X, Y}: A: {X, Y} -> add(Y1) -> rmv(X) -> {Y, Y1} B:
{X, Y} -> add(X1) -> rmv(Y) -> {X, X1}

Neste caso, o set final iria conter apenas {X1, Y1}, uma vez que depois de ser
feito add(X) e add(Y) é executado rmv(X) e rmv(Y) estes vão desaparecer no final

## No contexto de grafos, explique de que forma a variação do número de edges (mantendo fixo o número de vertices) pode afectar o diâmetro e o número de connected components.

Quanto menor for o número de Edges, maior vai ser o número de connected
components. (menos edges -> vértices com menos conexões -> mais componentes
separadas)

Quando menor for o número de edges, maior vai ser o diametro. (menos edges ->
caminhos mais longos entre diferentes vértices -> maior diametro)

## Na formação de uma spanning tree sobre um grafo conectado, e a partir de um nó iniciador, explique como esse mesmo nó pode determinar que a ávore está formada

Assuming i0 is the tree root. Every tree node is marked with parent = nil and
marked = False (True in root node i0)

First step: Process i0 sends a search message to unmarked children's leaves.
These leaves upon receiving a search message from i0 do the following:

- marked = True and set parent = i0
- In the next round, search messages are sent from these processes

After X steps: When a node doesn't have any children leave, they will send a
response to their parent. Parent terminates when all children terminate.
Responses are collected from leaves to the tree root.

## Indique de que modo um novo nó que se junta à rede Gnutella pode obter informação sobre quais outros nós pode tentar contactar

The node will connect to a Super Peer, which is highly available and have large
bandwidth. Once connected to a Super Peer, the node would request from the Super
Peer a list of addresses of other nodes in the network. The client would then
try to connect to those nodes, as well as to other nodes provided by the new
neighbour.

Not sure if the question is about Super Peer mechanism or ping pong

PING and PONG messages are used to discover new nodes.  
PINGs are flooded and PONGs are answered by along reverse  
paths.

