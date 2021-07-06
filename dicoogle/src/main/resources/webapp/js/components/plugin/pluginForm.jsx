import React, {PropTypes} from 'react';
import {ResultsSelected} from '../../stores/resultSelected';
import dicoogleClient from 'dicoogle-client';

const Dicoogle = dicoogleClient();

export default class PluginFormModal extends React.Component {

  static get propTypes() {
    return {
      slotId: PropTypes.string.isRequired,
      plugin: PropTypes.shape({
        name: PropTypes.string.isRequired,
        caption: PropTypes.string
      }),
      data: React.PropTypes.object,
      onHide: PropTypes.func.isRequired
    };
  }

  constructor(props) {
    super(props);
    this.handleMounted = this.handleMounted.bind(this);
    this.handleHideSignal = this.handleHideSignal.bind(this);
    this.handlePluginReady = this.handlePluginReady.bind(this);
  }

  onConfirm() {
    this.props.onHide();
  }

  handleMounted(component) {
    if (component) {
      const node = component;
      node.addEventListener('hide', this.handleHideSignal);
      node.addEventListener('plugin-ready', this.handlePluginReady);
      Dicoogle.emitSlotSignal(node, 'result-selection-ready', ResultsSelected.get());
    }
  }

  handleHideSignal({target}) {
      console.log('Plugin requested to hide');
      target.removeEventListener('hide', this.handleHideSignal);
      target.removeEventListener('plugin-ready', this.handlePluginReady);
      this.props.onHide();
  }

  handlePluginReady({target}){
    console.log("WebCore: got plugin-ready")
    if (target) {
      console.log("WebCore: about to emit ResultsSelected.get()")
      Dicoogle.emitSlotSignal(target, 'result-selection-ready', ResultsSelected.get());
    }
  }

  render() {
    const {plugin} = this.props;
    return (plugin &&
      <div>

          <dicoogle-slot {...this.props.data} ref={this.handleMounted} data-slot-id={this.props.slotId} data-plugin-name={plugin.name}>
            {plugin.name && <div className="loader-inner ball-pulse">
              <div/><div/><div/>
            </div>}
          </dicoogle-slot>

      </div>
    );
  }
}
