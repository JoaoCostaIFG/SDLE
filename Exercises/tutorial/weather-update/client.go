package main

import (
	"fmt"
	zmq "github.com/pebbe/zmq4"
	"os"
	"strconv"
	"strings"
)

func main() {
	subscriber, _ := zmq.NewSocket(zmq.SUB)
	defer subscriber.Close()
	subscriber.Connect("tcp://localhost:5555")

	// subscribe to zipcode
	filter := "10001 "
	if len(os.Args) > 1 {
		filter = os.Args[1] + " "
	}
	subscriber.SetSubscribe(filter)

	total_temp := 0
	update_nbr := 0
	for update_nbr < 100 {
		msg, _ := subscriber.Recv(0)

		if msgs := strings.Fields(msg); len(msgs) > 1 {
			if temperature, err := strconv.Atoi(msgs[1]); err == nil {
				total_temp += temperature
				update_nbr++
			}
		}
	}

	fmt.Printf("Average temperature for zip '%s' was %dF\n",
		strings.TrimSpace(filter), total_temp/update_nbr)
}
