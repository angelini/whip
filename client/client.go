package main

import (
	"bufio"
	"encoding/json"
	"github.com/nsf/termbox-go"
	"log"
	"net"
)

type SizeMessage struct {
	Width  int
	Height int
}

type KeyMessage struct {
	C    rune
	Alt  bool
	Ctrl bool
}

type Cell struct {
	C  rune
	Fg string
	Bg string
}

type DisplayMessage struct {
	Cursor struct {
		X int
		Y int
	}
	Content [][]Cell
}

func emit(conn net.Conn, message interface{}) {
	m, err := json.Marshal(message)
	if err != nil {
		log.Panic(err)
	}

	log.Printf("message-> %s\n", string(m[:]))
	conn.Write(m)
}

func emitSize(conn net.Conn) {
	width, height := termbox.Size()
	emit(conn, SizeMessage{width, height})
}

func emitKey(conn net.Conn, ch rune, key termbox.Key, mod termbox.Modifier) {
	emit(conn, KeyMessage{ch, mod == termbox.ModAlt, false})
}

func draw(message DisplayMessage) {
	for x, row := range message.Content {
		for y, cell := range row {
			termbox.SetCell(x, y, cell.C, termbox.ColorWhite, termbox.ColorBlack)
		}
	}

	termbox.SetCursor(message.Cursor.X, message.Cursor.Y)
	termbox.Flush()
}

func listen(conn net.Conn) {
	reader := bufio.NewReader(conn)

	for {
		reply, err := reader.ReadBytes('\n')
		if err != nil {
			log.Panic(err)
		}

		log.Printf("reply-> %s\n", string(reply[:]))

		var message DisplayMessage
		err = json.Unmarshal(reply, &message)
		if err != nil {
			log.Panic(err)
		}

		draw(message)
	}
}

func main() {
	err := termbox.Init()
	if err != nil {
		log.Panic(err)
	}
	defer termbox.Close()

	conn, err := net.Dial("tcp", "localhost:8080")
	if err != nil {
		log.Panic(err)
	}
	defer conn.Close()

	go listen(conn)
	emitSize(conn)

	for {
		ev := termbox.PollEvent()

		switch ev.Type {
		case termbox.EventKey:
			emitKey(conn, ev.Ch, ev.Key, ev.Mod)
		case termbox.EventResize:
			emitSize(conn)
		}

		if ev.Key == termbox.KeyEsc {
			return
		}
	}
}
