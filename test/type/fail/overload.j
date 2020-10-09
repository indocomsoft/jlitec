class A {
  Void main(){
    C c;
    c.f(null);
    return;
  }
}
class B { }
class C {
  Void f(B b) { return; }
  Void f(C c) { return; }
}
