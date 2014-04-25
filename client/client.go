package main

import (
	"bufio"
	"encoding/json"
	"github.com/nsf/termbox-go"
	"log"
	"net"
	"unicode/utf8"
)

type MessageWrapper struct {
	Type string      `json:"type"`
	Body interface{} `json:"body"`
}

type SizeMessage struct {
	Width  int `json:"width"`
	Height int `json:"height"`
}

type KeyMessage struct {
	C    string `json:"c"`
	Alt  bool   `json:"alt?"`
	Ctrl bool   `json:"ctrl?"`
}

type Cell struct {
	C  string `json:"c"`
	Fg string `json:"fg"`
	Bg string `json:"bg"`
}

type DisplayMessage struct {
	Cursor struct {
		X int `json:"x"`
		Y int `json:"y"`
	} `json:"cursor"`
	Content [][]Cell `json:"content"`
}

func parseKey(key termbox.Key) (c string, ctrl bool) {
	switch key {
	default:
		return ":unknown", false
	case termbox.KeyCtrlSpace:
		return " ", true
	case termbox.KeyArrowUp:
		return ":up", false
	case termbox.KeyArrowDown:
		return ":down", false
	case termbox.KeyArrowLeft:
		return ":left", false
	case termbox.KeyArrowRight:
		return ":right", false
	}
}

func messageType(body interface{}) string {
	switch body.(type) {
	default:
		return "unknown"
	case SizeMessage:
		return "size"
	case KeyMessage:
		return "key"
	}
}

func emit(conn net.Conn, body interface{}) {
	mtype := messageType(body)

	m, err := json.Marshal(MessageWrapper{mtype, body})
	if err != nil {
		log.Panic(err)
	}

	log.Printf("message-> %s\n", string(m[:]))
	conn.Write(append(m, '\n'))
}

func emitSize(conn net.Conn) {
	width, height := termbox.Size()
	emit(conn, SizeMessage{width, height})
}

func emitKey(conn net.Conn, c rune, key termbox.Key, mod termbox.Modifier) {
	cstring := string(c)
	ctrl := false

	if c == 0 {
		cstring, ctrl = parseKey(key)
	}

	emit(conn, KeyMessage{cstring, mod == termbox.ModAlt, ctrl})
}

func draw(message DisplayMessage) {
	for x, row := range message.Content {
		for y, cell := range row {
			r, _ := utf8.DecodeRuneInString(cell.C)
			termbox.SetCell(x, y, r, termbox.ColorWhite, termbox.ColorBlack)
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
			return
		}

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
