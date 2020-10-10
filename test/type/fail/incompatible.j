class A{
  Void main() {
    new B().f(this);
    return;
  }
}

class B {
  Void f(B a) {return;}
}
