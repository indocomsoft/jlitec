class A {
  Void main() {
    Foo foo;
    foo.bar = new Bar();
    foo.bar.qwe = new Qwe();
    println(foo.bar.qwe.i);
    foo.bar.qwe.i = 5;
    println(foo.bar.qwe.i);
    println(new Qwe().setI(100).i);
  }
}

class Foo {
  Bar bar;
}

class Bar {
  Qwe qwe;
}

class Qwe {
  Int i;

  Qwe setI(Int i) {
    this.i = i;
    return this;
  }
}
