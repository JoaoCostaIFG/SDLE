package subscriber

import zmq "github.com/pebbe/zmq4"

type SubscriberI interface {
	Destroy() error
	Connect(endpoint string) error
	Get() []string
	Subscribe(string) error
	Unsubscribe(string) error
}

type Subscriber struct {
	zctx *zmq.Context
	s    *zmq.Socket
}

func NewSubscriber(zctx *zmq.Context) (*Subscriber, error) {
	p := new(Subscriber)
	p.zctx = zctx
	s, e := p.zctx.NewSocket(zmq.SUB)
	p.s = s
	return p, e
}

func (p *Subscriber) Destroy() error {
	return p.s.Close()
}

func (p *Subscriber) Connect(endpoint string) error {
	return p.s.Connect(endpoint)
}

func (p *Subscriber) Get() []string {
	msg, _ := p.s.RecvMessage(0)
	return msg
}

func (p *Subscriber) Subscribe(topic string) error {
	return p.s.SetSubscribe(topic)
}

func (p *Subscriber) Unsubscribe(topic string) error {
	return p.s.SetUnsubscribe(topic)
}
