package src

import (
	"fmt"
	zmq "github.com/pebbe/zmq4"
	"math/rand"
	"src/publisher"
	"time"
)

func publish(zctx *zmq.Context, endpoint string) {
	p, err := publisher.NewPublisher(zctx)
	defer p.Destroy()
	if err != nil {
		fmt.Println("Create publisher error")
		return
	}

	err = p.Connect(endpoint)
	if err != nil {
		fmt.Println("Connect publisher error")
		return
	}

	// send random publishing
	rand.Seed(time.Now().UnixNano())
	for {
		zipcode := rand.Intn(100000)
		temperature := rand.Intn(215) - 80

		topic := fmt.Sprintf("%05d", zipcode)
		msg := fmt.Sprintf("%d", temperature)

		p.Put(topic, msg)
	}
}

func main() {
	zctx, _ := zmq.NewContext()
	defer zctx.Term()

	publish(zctx, "tcp://localhost:5559")
}
