class A{
  Void main() {
    return;
  }
}

class B {
  Void foo() {
    foo(this);
    return;
  }
  Void foo(B b) {
    return;
  }
}
