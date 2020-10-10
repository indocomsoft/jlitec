class A {
  Void main(){
    return;
  }
}
class B { }
class C {
  Void f() {
    f(null);
  }
  Void f(B b) { return; }
  Void f(C c) { return; }
}
