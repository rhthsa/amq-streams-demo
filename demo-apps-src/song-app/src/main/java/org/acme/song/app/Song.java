package org.acme.song.app;

public class Song {

    String id;
    String name;
    String author;
    Operation  op;

    public Song() {
    }

    public Song(String id, String name, String author) {
        this.id = id;
        this.name = name;
        this.author = author;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAuthor() {
        return author;
    }

    public void setOp(Operation op) {
        this.op = op;
    }

    public Operation getOp() {
        return op;
    }

    @Override
    public String toString() {
        return "Song [author=" + author + ", id=" + id + ", name=" + name + ", operation=" + op + "]";
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

}