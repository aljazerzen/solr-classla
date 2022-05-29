# CLASSLA exposed via gRPC

This project uses [poetry](https://python-poetry.org/) package manager.

Install dependencies:

    $ poetry install

There is a dependency resolution problem with `classla` at the moment. As a workadround, install it with pip:

    $ poetry shell
    $ pip install classla

Now you can run the server:

    $ poetry run python src/main.py

Test the server with:

    $ poetry run python src/analyzer_cli.py

After `/proto/classla.proto` file is changed, you should update protobuffer definitions in the project:

    $ poetry run python -m grpc_tools.protoc -I ../proto --python_out=src --grpc_python_out=src ../proto/classla.proto

