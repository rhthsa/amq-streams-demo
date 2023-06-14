package org.acme.song.indexer.app;

public class Song {

    String id;
    String name;
    String author;
    Operation  op;

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

}