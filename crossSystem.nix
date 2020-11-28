with import <nixpkgs> {
  crossSystem = {
      config = "armv7l-unknown-linux-gnueabi";
  };
};

mkShell {
  buildInputs = [ zlib glibc.static ]; # your dependencies here
}
