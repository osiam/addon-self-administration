#!/bin/bash -ex

case $CIRCLE_NODE_INDEX in
    0)
        mvn verify -P integration-tests -Dit.test=ChangeEmailIT -Ddocker.removeIntermediateImages=false
        ;;
    1)
        mvn verify -P integration-tests -Dit.test=LostPasswordIT -Ddocker.removeIntermediateImages=false
        ;;
    2)
        mvn verify -P integration-tests -Dit.test=RegistrationIT -Ddocker.removeIntermediateImages=false
        ;;
esac
