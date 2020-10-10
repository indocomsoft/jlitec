class A{
  Void main() {
    B b;
    if (true) {b.a();} else {b.b();}
    return;
  }
}

class B {
  Int a() { return 5; }
  String b() { return null; }
}
