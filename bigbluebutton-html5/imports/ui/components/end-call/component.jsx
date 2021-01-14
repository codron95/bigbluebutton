import React, { Component } from 'react';
import cx from 'classnames';
import Button from '/imports/ui/components/button/component';
import {
  defineMessages, injectIntl
} from 'react-intl';
import { styles } from './styles.scss';

const intlMessages = defineMessages({
  endCallTitle: {
    id: 'app.endCall.title',
    description: 'End call'
  }
});

class EndCall extends Component {
  constructor(props) {
    super(props);
  }

  render() {
    return (
      <Button
        id="end-call"
        className={styles.endCallButton}
        icon="logout"
        size="lg"
        circle
        color="danger"
      />
    );
  }
}

export default injectIntl(EndCall);
