package main

import (
    zmq "github.com/pebbe/zmq4"
    "github.com/nsf/termbox-go"
    "fmt"
)

func draw(reply []string) {
    fmt.Printf("reply %s\n", reply)
}

func main () {
    err := termbox.Init()
    if err != nil {
        panic(err)
    }
    defer termbox.Close()

    client, err := zmq.NewSocket(zmq.REQ)
    if err != nil {
        panic(err)
    }
    client.Connect("tcp://127.0.0.1:8080")

    go func() {
        reply, err := client.RecvMessage(0)
        if err != nil {
            panic(err)
        }

        draw(reply)
    }()

loop:
    for {
        switch ev := termbox.PollEvent(); ev.Type {
        case termbox.EventKey:
            switch ev.Key {
            case termbox.KeyEsc:
                break loop
            }
        case termbox.EventError:
            panic(ev.Err)
        }

    }
}
